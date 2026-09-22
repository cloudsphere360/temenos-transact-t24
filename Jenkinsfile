pipeline {
    // -------------------------------------------------------------------------
    // AGENT SPECIFICATION
    // Ensures this pipeline strictly runs on your dedicated target worker node
    // -------------------------------------------------------------------------
    agent {
        node {
            label 'Jenkins Server Agent-1' // Target worker node where tools (mvn, docker, aws, kubectl) are configured
        }
    }

    stages {
        // ---------------------------------------------------------------------
        // STAGE 1: SOURCE CODE CHECKOUT
        // Pulls the latest commits from the Git repository into the agent workspace
        // ---------------------------------------------------------------------
        stage('Step 1: Checkout Source Code') {
            steps {
                echo 'Checking out latest code from master branch...'
                checkout scm // Clones the Git repository branch configured in the Jenkins job
                echo 'Checkout completed successfully.'
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 2: AUTOMATED MAVEN BUILD ACROSS ALL MICROSERVICES
        // Finds every microservice pom.xml dynamically and compiles it
        // ---------------------------------------------------------------------
        stage('Step 2: Automated Maven Build') {
            steps {
                echo 'Starting Maven build for all microservice modules...'
                sh '''
                    # 1. Check if Maven CLI is installed and available in the system PATH
                    mvn -version

                    # 2. Search recursively inside the 'sources' folder for every pom.xml
                    # Why: Each microservice (e.g. Settlement, CustomerAPI) has its own independent pom.xml
                    find sources -name "pom.xml" | while read pom; do
                        
                        # Extract directory path of the current pom.xml (e.g. sources/API's/createCustomerAPI)
                        module_dir=\((dirname "\)pom")
                        
                        echo "=========================================================="
                        echo "Building module in: $module_dir"
                        echo "Using POM configuration: $pom"
                        echo "=========================================================="
                        
                        # -f "$pom": Explicitly specifies the exact pom.xml file location to Maven
                        # clean: Cleans up any prior compiled files or stale build cache
                        # package: Compiles Java sources and packages them into .jar or .war based on  tag
                        # -DskipTests: Skips unit test execution to ensure fast, unblocked builds
                        mvn -f "$pom" clean package -DskipTests
                    done
                '''
            }
        }

        // ---------------------------------------------------------------------
        // STAGE 3: INSPECT & VERIFY BUILT ARTIFACTS
        // Verifies that binaries (.jar / .war) were actually created in target/ folders
        // ---------------------------------------------------------------------
        stage('Step 3: Inspect Generated Artifacts') {
            steps {
                echo 'Listing all generated JAR and WAR files in module target directories...'
                sh '''
                    # Search inside 'sources' for all generated .jar and .war files excluding unit test folders
                    echo "--- Generated Artifacts List ---"
                    find sources -type f \(-name "*.jar" -o -name "*.war"\) ! -path "*/test/*"
                '''
            }
        }
    }

    // -------------------------------------------------------------------------
    // POST-BUILD ACTIONS
    // Notifications and status handling after stages complete
    // -------------------------------------------------------------------------
    post {
        success {
            echo 'SUCCESS: Step 2 Maven compilation successfully completed. Binaries are ready.'
        }
        failure {
            echo 'FAILED: Maven build failed. Check compilation logs above for details.'
        }
    }
}