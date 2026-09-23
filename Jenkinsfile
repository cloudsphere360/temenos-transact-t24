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

        stage('Check Repository') {
    steps {
        sh '''
            echo "===== ROOT FILES ====="
            ls -la

            echo "===== DOCKERFILES ====="
            find . -iname "Dockerfile*" -type f

            echo "===== BUILD SCRIPTS ====="
            find . -iname "*.sh" -type f
        '''
         }
       }
    }
}