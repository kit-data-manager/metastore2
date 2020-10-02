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
FROM openjdk:11-stretch AS build-env-java
MAINTAINER webmaster@datamanager.kit.edu
LABEL stage=build-env

RUN apt-get update && \
    apt-get upgrade --assume-yes && \
    apt-get install --assume-yes git 

####################################################
# Building service
####################################################
FROM build-env-java AS build-metastore2
MAINTAINER webmaster@datamanager.kit.edu
LABEL stage=build-contains-sources

# Fetch arguments from above
ARG REPO_NAME_DEFAULT
ARG SERVICE_ROOT_DIRECTORY_DEFAULT

# Declare environment variables
ENV REPO_NAME=${REPO_NAME_DEFAULT}
ENV SERVICE_DIRECTORY=$SERVICE_ROOT_DIRECTORY_DEFAULT$REPO_NAME

RUN echo $REPO_NAME
RUN echo ${REPO_NAME:-"nicht definiert"}
RUN echo $SERVICE_DIRECTORY
RUN echo ${SERVICE_DIRECTORY:-"nicht definiert"}
RUN mkdir -p /git/${REPO_NAME}
WORKDIR /git/${REPO_NAME}
COPY . .
RUN chmod +x build.sh
RUN bash ./build.sh $SERVICE_DIRECTORY

####################################################
# Runtime environment
####################################################
FROM openjdk:11-stretch AS run-metastore2
MAINTAINER webmaster@datamanager.kit.edu
LABEL stage=run

# Fetch arguments from above
ARG REPO_NAME_DEFAULT
ARG REPO_PORT_DEFAULT
ARG SERVICE_ROOT_DIRECTORY_DEFAULT

# Declare environment variables
ENV REPO_NAME=${REPO_NAME_DEFAULT}
ENV SERVICE_DIRECTORY=${SERVICE_ROOT_DIRECTORY_DEFAULT}${REPO_NAME}
ENV REPO_PORT=${REPO_PORT_DEFAULT}

RUN mkdir -p /git/${REPO_NAME}
WORKDIR /git/${REPO_NAME}
COPY --from=build-metastore2 ${SERVICE_DIRECTORY}/* ./

EXPOSE ${REPO_PORT}
ENTRYPOINT ["sh", "/bin/bash"]
