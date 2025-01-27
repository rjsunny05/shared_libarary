def call(Map config) {
    pipeline {
        agent any
        environment {
            SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
            ENVIRONMENT = config.ENVIRONMENT
            CODE_BASE_PATH = config.CODE_BASE_PATH
            ACTION_MESSAGE = config.ACTION_MESSAGE
            KEEP_APPROVAL_STAGE = config.KEEP_APPROVAL_STAGE
        }
        stages {
            stage('Clone Repository') {
                steps {
                    echo "Cloning repository from ${CODE_BASE_PATH}"
                    checkout scm
                }
            }
            stage('User Approval') {
                when {
                    expression { KEEP_APPROVAL_STAGE.toBoolean() }
                }
                steps {
                    input message: "Approve to proceed with execution for ${ENVIRONMENT}?", 
                          ok: "Yes, proceed"
                }
            }
            stage('Playbook Execution') {
                steps {
                    echo "Running Ansible Playbook for ${ENVIRONMENT}"
                    sh """
                    ansible-playbook -i ${CODE_BASE_PATH}/inventory.ini ${CODE_BASE_PATH}/.yml
                    """
                }
            }
            stage('Send Notification') {
                steps {
                    echo "Sending notification to Slack: ${SLACK_CHANNEL}"
                    slackSend channel: SLACK_CHANNEL, message: "${ACTION_MESSAGE}"
                }
            }
        }
    }
}
