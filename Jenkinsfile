pipeline {
    agent any

    tools {
        maven 'Maven-3'
    }

    environment {
        PATH = "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

        IMAGE_NAME = "pranavjambare/employee-app"
        IMAGE_TAG  = "${BUILD_NUMBER}"

        SONAR_HOST = "SonarQube"
    }

    options {
        timestamps()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                sh '''
                    java -version
                    mvn -version
                    docker --version
                    trivy --version
                '''
            }
        }

        stage('Compile') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh './mvnw test'
            }
        }

        stage('Package') {
            steps {
                sh './mvnw package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONAR_HOST}") {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            ./mvnw sonar:sonar \
                              -Dsonar.token=$SONAR_TOKEN
                        '''
                    }
                }
            }
        }

        stage('Trivy File System Scan') {
            steps {
                sh '''
                    trivy fs . \
                    --severity HIGH,CRITICAL \
                    --exit-code 0
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build \
                    -t ${IMAGE_NAME}:${IMAGE_TAG} \
                    -t ${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Trivy Image Scan') {
            steps {
                sh """
                    trivy image ${IMAGE_NAME}:${IMAGE_TAG} \
                    --severity HIGH,CRITICAL \
                    --exit-code 0
                """
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: '28658194-41d0-467e-b36f-b7563a12baff',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login \
                        -u "$DOCKER_USER" \
                        --password-stdin
                    '''
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                sh """
                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${IMAGE_NAME}:latest
                """
            }
        }

        stage('Clone K8s Repository') {
            steps {
                dir('employee-app-k8s') {
                    git(
                        branch: 'main',
                        credentialsId: 'f24fbff2-af87-4123-a0cb-2a9226841d92',
                        url: 'https://github.com/pra9jambare/employee-app-k8s.git'
                    )
                }
            }
        }

        stage('Update Deployment Manifest') {
            steps {
                dir('employee-app-k8s') {
                    sh """
                        sed -i '' 's#image: .*#image: ${IMAGE_NAME}:${IMAGE_TAG}#' deployment.yaml

                        echo "Updated deployment.yaml"

                        cat deployment.yaml
                    """
                }
            }
        }

        stage('Commit & Push Manifest') {
            steps {
                dir('employee-app-k8s') {
                    sh """
                        git config user.name "Jenkins"
                        git config user.email "jenkins@local"

                        git add deployment.yaml

                        git commit -m "Update image to ${IMAGE_TAG}" || true

                        git push origin main
                    """
                }
            }
        }
    }

    post {
        success {
            echo "===================================="
            echo "Build Successful"
            echo "Docker Image : ${IMAGE_NAME}:${IMAGE_TAG}"
            echo "GitOps Manifest Updated"
            echo "Argo CD will automatically deploy."
            echo "===================================="
        }

        failure {
            echo "Build Failed"
        }

        always {
            cleanWs()
        }
    }
}
