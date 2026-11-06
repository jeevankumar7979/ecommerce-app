def allServices = [
    'config-server', 'discovery-server', 'api-gateway', 'order-service',
    'inventory-service', 'payment-service', 'notification-service'
]
def deployableServices = [
    'config-server', 'api-gateway', 'order-service',
    'inventory-service', 'payment-service', 'notification-service'
]

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        IMAGE_REGISTRY = 'ghcr.io/jeevankumar7979/ecommerce-app'
        KUBE_NAMESPACE = 'ecommerce'
    }

    stages {
        stage('Checkout and detect changes') {
            steps {
                checkout scm
                script {
                    def changedPaths = []
                    currentBuild.changeSets.each { changeSet ->
                        changeSet.items.each { change ->
                            change.affectedFiles.each { file ->
                                changedPaths << file.path
                            }
                        }
                    }

                    def buildServices = [] as Set
                    def deployServices = [] as Set
                    def configChanged = false
                    def platformChanged = currentBuild.number == 1
                    def sharedBuildChanged = false

                    if (currentBuild.number == 1) {
                        buildServices.addAll(allServices)
                    }

                    changedPaths.each { path ->
                        def service = allServices.find { path.startsWith("${it}/") }
                        if (service != null) {
                            buildServices.add(service)
                        } else if (path.startsWith('config/')) {
                            configChanged = true
                        } else if (path == 'k8s/platform.yaml') {
                            platformChanged = true
                        } else if (path.startsWith('gradle/') ||
                                   path == 'settings.gradle' ||
                                   path == 'gradlew' ||
                                   path == 'gradlew.bat' ||
                                   path == '.dockerignore' ||
                                   path == 'scripts/build-all.sh') {
                            sharedBuildChanged = true
                        }
                    }

                    if (sharedBuildChanged || platformChanged) {
                        buildServices.addAll(allServices)
                    }
                    if (platformChanged || configChanged) {
                        deployServices.addAll(deployableServices)
                    } else {
                        deployServices.addAll(buildServices.findAll { deployableServices.contains(it) })
                    }

                    env.BUILD_SERVICES = buildServices.join(',')
                    env.DEPLOY_SERVICES = deployServices.join(',')
                    env.CONFIG_CHANGED = configChanged.toString()
                    env.PLATFORM_CHANGED = platformChanged.toString()
                    echo "Changed paths: ${changedPaths}"
                    echo "Services to build: ${env.BUILD_SERVICES ?: '(none)'}"
                    echo "Services to deploy: ${env.DEPLOY_SERVICES ?: '(none)'}"
                }
            }
        }

        stage('Test and package') {
            when {
                expression { return (env.BUILD_SERVICES ?: '').trim().length() > 0 }
            }
            steps {
                script {
                    env.BUILD_SERVICES.split(',').each { service ->
                        echo "Testing and packaging ${service}"
                        sh "gradle --no-daemon -p '${service}' clean test bootJar"
                    }
                }
            }
        }

        stage('Build container images') {
            when {
                expression { return (env.BUILD_SERVICES ?: '').trim().length() > 0 }
            }
            steps {
                script {
                    env.BUILD_SERVICES.split(',').each { service ->
                        sh "docker build --file ${service}/Dockerfile --tag ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} ."
                    }
                }
            }
        }

        stage('Publish images') {
            when {
                expression {
                    def branchName = (env.BRANCH_NAME ?: env.GIT_BRANCH ?: '')
                        .replaceFirst('^refs/heads/', '')
                        .replaceFirst('^refs/remotes/origin/', '')
                        .replaceFirst('^origin/', '')
                    return (branchName == 'main' || branchName == 'origin/main') &&
                        (env.BUILD_SERVICES ?: '').trim().length() > 0
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-credentials',
                        usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
                    sh '''
                        set +x
                        printf '%s' "$REGISTRY_PASSWORD" | docker login ghcr.io \
                          --username "$REGISTRY_USER" --password-stdin
                    '''
                    script {
                        env.BUILD_SERVICES.split(',').each { service ->
                            sh "docker tag ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} ${IMAGE_REGISTRY}/${service}:latest"
                            sh "docker push ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER}"
                            sh "docker push ${IMAGE_REGISTRY}/${service}:latest"
                        }
                    }
                    sh 'docker logout ghcr.io'
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                expression {
                    def branchName = (env.BRANCH_NAME ?: env.GIT_BRANCH ?: '')
                        .replaceFirst('^refs/heads/', '')
                        .replaceFirst('^refs/remotes/origin/', '')
                        .replaceFirst('^origin/', '')
                    return (branchName == 'main' || branchName == 'origin/main') &&
                        ((env.DEPLOY_SERVICES ?: '').trim().length() > 0 ||
                         env.CONFIG_CHANGED == 'true' ||
                         env.PLATFORM_CHANGED == 'true')
                }
            }
            steps {
                withCredentials([file(credentialsId: 'ecommerce-kubeconfig', variable: 'KUBECONFIG')]) {
                    script {
                        if (env.PLATFORM_CHANGED == 'true') {
                            sh 'kubectl apply -f k8s/platform.yaml'
                        }
                        if (env.CONFIG_CHANGED == 'true' || env.PLATFORM_CHANGED == 'true') {
                            sh 'kubectl create configmap service-config --namespace ecommerce --from-file=config/ --dry-run=client -o yaml | kubectl apply -f -'
                        }

                        def servicesToDeploy = env.DEPLOY_SERVICES
                            ? env.DEPLOY_SERVICES.split(',')
                            : []
                        if (env.CONFIG_CHANGED == 'true' && env.PLATFORM_CHANGED != 'true') {
                            servicesToDeploy.each { service ->
                                sh "kubectl rollout restart deployment/${service} --namespace ${KUBE_NAMESPACE}"
                                sh "kubectl rollout status deployment/${service} --namespace ${KUBE_NAMESPACE} --timeout=180s"
                            }
                        } else {
                            servicesToDeploy.each { service ->
                                sh "kubectl set image deployment/${service} app=${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} --namespace ${KUBE_NAMESPACE}"
                            }
                            servicesToDeploy.each { service ->
                                sh "kubectl rollout status deployment/${service} --namespace ${KUBE_NAMESPACE} --timeout=180s"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout ghcr.io >/dev/null 2>&1 || true'
        }
    }
}
