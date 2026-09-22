pipeline {
    agent {
        node {
            label 'Jenkins Server Agent-1'
        }
    }

    stages {
        stage('Step 1: Checkout Source Code') {
            steps {
                echo 'Checking out latest code from master branch...'
                checkout scm
                echo 'Checkout completed successfully.'
            }
        }

        stage('Step 2: Automated Maven Build') {
            steps {
                echo 'Starting Maven build for all microservice modules...'
                sh '''#!/bin/bash
                    set -e
                    echo "Checking Maven version:"
                    mvn -version

                    # Loop through all pom.xml files found in sources directory
                    for pom in $(find sources -name "pom.xml"); do
                        echo "=========================================================="
                        echo "Processing POM: ${pom}"
                        echo "=========================================================="
                        mvn -f "${pom}" clean package -DskipTests
                    done
                '''
            }
        }

        stage('Step 3: Inspect Generated Artifacts') {
            steps {
                echo 'Listing all generated JAR and WAR files in module target directories...'
                sh '''#!/bin/bash
                    echo "--- Generated Artifacts List ---"
                    find sources -type f \(-name "*.jar" -o -name "*.war"\) ! -path "*/test/*"
                '''
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Step 2 Maven compilation successfully completed.'
        }
        failure {
            echo 'FAILED: Maven build failed. Check logs above for errors.'
        }
    }
}