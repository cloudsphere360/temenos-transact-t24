pipeline {
    agent {
        node {
            label 'Jenkins Server Agent-1'
        }
    }

    stages {
        stage('Step 1: Checkout Source Code') {
            steps {
                echo 'Connecting to repository and checking out master branch...'
                checkout scm
                echo 'Checkout completed successfully.'
            }
        }
    }
}