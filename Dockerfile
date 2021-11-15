####################################################
# START GLOBAL DECLARATION
####################################################
ARG REPO_NAME_DEFAULT=metastore2
ARG REPO_PORT_DEFAULT=8040
ARG SERVICE_ROOT_DIRECTORY_DEFAULT=/spring/
####################################################
# END GLOBAL DECLARATION
####################################################

####################################################
# Building environment (java & git)
####################################################
FROM openjdk:16-alpine AS build-env-java
LABEL maintainer=webmaster@datamanager.kit.edu
LABEL stage=build-env

# Install git as additional requirement
RUN apk update && \
    apk upgrade  && \
    apk add  git && \
    apk add  bash

####################################################
# Building service
####################################################
FROM build-env-java AS build-service-metastore2
LABEL maintainer=webmaster@datamanager.kit.edu
LABEL stage=build-contains-sources

# Fetch arguments from above
ARG REPO_NAME_DEFAULT
ARG SERVICE_ROOT_DIRECTORY_DEFAULT

# Declare environment variables
ENV REPO_NAME=${REPO_NAME_DEFAULT}
ENV SERVICE_DIRECTORY=$SERVICE_ROOT_DIRECTORY_DEFAULT$REPO_NAME

# Create directory for repo
RUN mkdir -p /git/${REPO_NAME}
WORKDIR /git/${REPO_NAME}
COPY . .
RUN cp settings/application-docker.properties settings/application-default.properties
# Build service in given directory
RUN bash ./build.sh $SERVICE_DIRECTORY

####################################################
# Runtime environment 4 metastore2
####################################################
FROM openjdk:16-alpine AS run-service-metastore2
LABEL maintainer=webmaster@datamanager.kit.edu
LABEL stage=run

# Fetch arguments from above
ARG REPO_NAME_DEFAULT
ARG REPO_PORT_DEFAULT
ARG SERVICE_ROOT_DIRECTORY_DEFAULT

# Declare environment variables
ENV REPO_NAME=${REPO_NAME_DEFAULT}
ENV SERVICE_DIRECTORY=${SERVICE_ROOT_DIRECTORY_DEFAULT}${REPO_NAME}
ENV REPO_PORT=${REPO_PORT_DEFAULT}

# Install bash as additional requirement
RUN apk update && \
    apk upgrade  && \
    apk add  bash

# Copy service from build container
RUN mkdir -p ${SERVICE_DIRECTORY}
WORKDIR ${SERVICE_DIRECTORY}
COPY --from=build-service-metastore2 ${SERVICE_DIRECTORY} ./

# Define repo port 
EXPOSE ${REPO_PORT}
ENTRYPOINT ["bash", "./run.sh"]
