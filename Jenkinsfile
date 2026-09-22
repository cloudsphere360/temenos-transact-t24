pipeline {
    agent {
        label 'Jenkins Server Agent-1'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Inspect Changes') {
            steps {
                sh '''
                    echo "===== WORKSPACE ====="
                    pwd

                    echo "===== CHANGED FILES ====="
                    git diff --name-only HEAD~1 HEAD

                    echo "===== POM FILES ====="
                    find . -name "pom.xml" -type f
                '''
            }
        }
    }
}