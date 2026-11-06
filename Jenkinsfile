pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        IMAGE_REGISTRY = 'ghcr.io/jeevankumar7979/ecommerce-app'
        KUBE_NAMESPACE = 'ecommerce'
    }

    stages {
        stage('Test and package') {
            steps {
                sh './scripts/build-all.sh'
            }
        }

        stage('Build container images') {
            steps {
                script {
                    ['config-server', 'discovery-server', 'api-gateway', 'order-service',
                     'inventory-service', 'payment-service', 'notification-service'].each { service ->
                        sh "docker build --file ${service}/Dockerfile --tag ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} ."
                    }
                }
            }
        }

        stage('Publish images') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-credentials',
                        usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
                    sh '''
                        set +x
                        printf '%s' "$REGISTRY_PASSWORD" | docker login ghcr.io \
                          --username "$REGISTRY_USER" --password-stdin
                    '''
                    script {
                        ['config-server', 'discovery-server', 'api-gateway', 'order-service',
                         'inventory-service', 'payment-service', 'notification-service'].each { service ->
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
            when { branch 'main' }
            steps {
                withCredentials([file(credentialsId: 'ecommerce-kubeconfig', variable: 'KUBECONFIG')]) {
                    sh 'kubectl apply -f k8s/platform.yaml'
                    script {
                        ['config-server', 'api-gateway', 'order-service',
                         'inventory-service', 'payment-service', 'notification-service'].each { service ->
                            sh "kubectl set image deployment/${service} app=${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} --namespace ${KUBE_NAMESPACE}"
                            sh "kubectl rollout status deployment/${service} --namespace ${KUBE_NAMESPACE} --timeout=180s"
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
