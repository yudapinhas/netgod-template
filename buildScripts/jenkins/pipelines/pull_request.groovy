@Library('kenshoo-jenkins-shared-lib@master') _
pipeline {
    agent { label 'microcosm-ubuntu-base' }

    options {
        ansiColor('xterm')
        timestamps()
    }
    environment {
        PATH = "$PWD:$WORKSPACE:$HOME/.pulumi/bin:$PATH"
        ARTIFACTORY_CREDS = credentials('jcasc_deployer-core')
        ARTIFACTORY_USER = "${env.ARTIFACTORY_CREDS_USR}"
        ARTIFACTORY_PASS = "${env.ARTIFACTORY_CREDS_PSW}"
        PULUMI_ACCESS_TOKEN = credentials('pulumi_access_token')
        AWS_ACCESS_KEY_ID = credentials('aws-master-key')
        AWS_SECRET_ACCESS_KEY = credentials('aws-master-secret')
        VAULT_TOKEN = credentials('prod-vault-root-token')
        VAULT_ADDR = credentials('jenkins-vault-url')
        VAULT_PROD_URL = credentials('jenkins-vault-url')
        OKTA_API_TOKEN = credentials('okta_token')
        aws_sdlc_ks_key = credentials('aws_sdlc_access_key')
        aws_sdlc_ks_secret = credentials('aws_sdlc_secret_key')
        VAULT_STAGING_CREDS = credentials('vault_jenkins_privileged')
        VAULT_STAGING_URL = credentials('vault-staging-url')
        VAULT_STAGING_ROLE_ID = "${env.VAULT_STAGING_CREDS_USR}"
        VAULT_STAGING_SECRET_ID = "${env.VAULT_STAGING_CREDS_PSW}"
        VAULT_PROD_CREDS = credentials('vault_prod_sre_jenkins')
        VAULT_PROD_ROLE_ID = "${env.VAULT_PROD_CREDS_USR}"
        VAULT_PROD_SECRET_ID = "${env.VAULT_PROD_CREDS_PSW}"
        GITHUB_TOKEN = credentials('kgithub-build-jenkins-core')
        CICD = 1
    }
    stages {
        stage('Clone Git Repository') {
            steps {
                cleanWs()
                checkout scm
            }
        }
    
        stage('Pre-Configuration & Testing') {
            steps {
                installPulumiBinary()
                configFileProvider([configFile(fileId: 'core-init-gradle', replaceTokens: true, targetLocation: "/home/ubuntu/.gradle/init.gradle")]){
                sh '''#!/bin/bash
                    set -xe

                    virtualenv -p python3.9 venv
                    source venv/bin/activate

                    aws codeartifact login --tool pip --domain skai --domain-owner 706083814122 --repository skai-python --region us-east-1
                    pip install -r requirements-dev.txt
                    
                    invoke unit-test --env-keys \$ghprbPullId

                    invoke integration-test --env-keys \$ghprbPullId

                    invoke regression-test --env-keys \$ghprbPullId

                    invoke e2e-test --env-keys \$ghprbPullId
          '''
                }
            }
        }
    }
    post {
        failure {
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
                recipientProviders: [[$class: 'DevelopersRecipientProvider']]
            )
        }
        cleanup {
            deleteDir()
            cleanWs()
        }
    }
}
