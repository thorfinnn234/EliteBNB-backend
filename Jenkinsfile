pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                bat 'mvnw.cmd test'
            }
        }

        stage('Build') {
            steps {
                bat 'mvnw.cmd clean package -DskipTests'
            }
        }
    }

    post {
        success {
            echo 'EliteBNB backend build successful!'
        }

        failure {
            echo 'EliteBNB backend build failed!'
        }
    }
}