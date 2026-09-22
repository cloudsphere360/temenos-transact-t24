pipeline {
    agent {
        node {
            label 'Jenkins Server Agent-1'
        }
    }

    stages {
        stage('Step 1: Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('Step 2: Automated Maven Build') {
            steps {
                sh '''#!/bin/bash
                    set -e
                    mvn -version

                    for pom in $(find sources -name "pom.xml"); do
                        echo "Building POM: ${pom}"
                        mvn -f "${pom}" clean package -DskipTests
                    done
                '''
            }
        }

        stage('Step 3: Inspect Generated Artifacts') {
            steps {
                sh '''#!/bin/bash
                    echo "--- Generated JAR and WAR Artifacts ---"
                    find sources -name "*.jar" -o -name "*.war" | grep -v "/test/"
                '''
            }
        }
    }
}