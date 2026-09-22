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

        stage('Step 2: Check TAFJ & T24 Lib Paths on Agent') {
            steps {
                sh '''#!/bin/bash
                    echo "Checking Preimage Kit directories for jars..."
                    ls -d /home/ec2-user/UAT/Preimage_Kits/* 2>/dev/null || echo "Path not found"
                    
                    echo "Searching for TAFJCore.jar on agent..."
                    find /home/ec2-user/UAT/ -name "TAFJCore.jar" 2>/dev/null || echo "TAFJCore not found"
                '''
            }
        }
    }
}