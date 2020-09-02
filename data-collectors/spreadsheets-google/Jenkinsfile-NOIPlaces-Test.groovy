pipeline {
    agent any
    
    environment {
        PROJECT = "spreadsheets-google"
        PROJECT_FOLDER = "data-collectors/${PROJECT}"
        LOG_FOLDER = "/var/log/opendatahub/data-collectors"
        ARTIFACT_NAME = "dc-${PROJECT}"
        NOIPLACES_CONFIG=credentials('spreadsheets.noiPlaces.config')
        GOOGLE_SECRET=credentials('spreadsheets.client_secret.json')
        KEYCLOAK_CONFIG=credentials('test-authserver-datacollector-client-config')
    }

    stages {
        stage('Configure') {
            steps {
                sh 'sed -i -e "s%\\(log4j.rootLogger\\s*=\\).*\\$%\\1DEBUG,R%" ${PROJECT_FOLDER}/src/main/resources/log4j.properties'
                sh 'cat ${GOOGLE_SECRET} > ${PROJECT_FOLDER}/src/main/resources/META-INF/spring/client_secret.json'
                sh 'cat "${NOIPLACES_CONFIG}" > ${PROJECT_FOLDER}/src/main/resources/META-INF/spring/application.properties'
                sh '''sed -i -e "s%\\(provenance.version\\s*=\\).*\\?%\\1$(xmlstarlet sel -N pom=http://maven.apache.org/POM/4.0.0 -t -v '/pom:project/pom:version' ${PROJECT_FOLDER}/pom.xml;)%" ${PROJECT_FOLDER}/src/main/resources/META-INF/spring/application.properties'''
                sh '''sed -i -e "s%\\(provenance.name\\s*=\\).*\\?%\\1$(xmlstarlet sel -N pom=http://maven.apache.org/POM/4.0.0 -t -v '/pom:project/pom:artifactId' ${PROJECT_FOLDER}/pom.xml;)%" ${PROJECT_FOLDER}/src/main/resources/META-INF/spring/application.properties'''
                sh 'sed -i -e "s%\\(log4j.appender.R.File\\s*=\\).*\\$%\\1${LOG_FOLDER}/${ARTIFACT_NAME}-${VENDOR}.log%" ${PROJECT_FOLDER}/src/main/resources/log4j.properties'
                sh 'cat ${KEYCLOAK_CONFIG} > .env'
                sh 'echo "collector=${PROJECT}" >> .env'
            }
        }
        stage('Build') {
            steps {
                sh 'cd data-collectors/spreadsheets-google && docker-compose -f docker-compose.NOIPlaces.yml build'
            }
        }
        stage('Deploy') {
            steps {
                sh 'cd data-collectors/spreadsheets-google && docker-compose --context test-docker -f docker-compose.NOIPlaces.yml up -d'
            }
        }
    }
}