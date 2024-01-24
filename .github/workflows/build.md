on:
push:
branches:
- feature/*
- develop workflow_dispatch:
permissions:
contents: read

env:
NODE_VERSION: '14.x' APPLICATION_NAME: 'zeeven' APPLICATION_TYPE: 'ms'

jobs:
create-prod-folder:
name: create prod folder runs-on: ubuntu-latest if: ${{ github.base_ref == 'master' || github.ref == 'refs/heads/master'
}} environment:
name: production steps:
- name: Checkout repository uses: actions/checkout@v3 - name: Create prod folder uses: appleboy/ssh-action@master with:
host: ${{ secrets.APPLICATIONS_HOST }} port: ${{ secrets.APPLICATIONS_PORT }} username: ${{
secrets.APPLICATIONS_USERNAME }} key: ${{ secrets.APPLICATIONS_SSH_PRIVATE_KEY }} script: | sudo mkdir -p
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}} sudo chmod ugo+rwx
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}

      - name: update configs
        run: |
          sed -i 's|IMAGE_NAME|simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}:${{ github.sha }}|' docker-compose.yml

      - name: copy docker compose
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.APPLICATIONS_HOST }}
          port: ${{ secrets.APPLICATIONS_PORT }}
          username: ${{ secrets.APPLICATIONS_USERNAME }}
          key: ${{ secrets.APPLICATIONS_SSH_PRIVATE_KEY }}
          source: 'docker-compose.yml'
          target: '/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}'

create-staging-folder:
runs-on: ubuntu-latest if: ${{ !(github.base_ref == 'master' || github.ref == 'refs/heads/master') }} environment:
name: staging steps:
- name: Checkout repository uses: actions/checkout@v3 - name: Create staging folder uses: appleboy/ssh-action@master
with:
host: ${{ secrets.APPLICATIONS_HOST }} port: ${{ secrets.APPLICATIONS_PORT }} username: ${{
secrets.APPLICATIONS_USERNAME }} key: ${{ secrets.APPLICATIONS_SSH_PRIVATE_KEY }} script: | sudo mkdir -p
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}} sudo chmod ugo+rwx
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}

      - name: update configs
        run: |
          sed -i 's|IMAGE_NAME|simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}:${{ github.sha }}|' docker-compose.yml

      - name: copy docker compose
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.APPLICATIONS_HOST }}
          port: ${{ secrets.APPLICATIONS_PORT }}
          username: ${{ secrets.APPLICATIONS_USERNAME }}
          key: ${{ secrets.APPLICATIONS_SSH_PRIVATE_KEY }}
          source: 'docker-compose.yml'
          target: '/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}'

dockerize:
runs-on: ubuntu-latest steps:
- name: Checkout repository uses: actions/checkout@v3 - name: Set up QEMU uses: docker/setup-qemu-action@v2 - name: Set
up Docker Buildx uses: docker/setup-buildx-action@v2

      - name: Login to DockerHub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'adopt'
      - name: Build with Maven
        run: mvn -Dmaven.test.skip=true clean compile package

      - name: Extract metadata (tags, labels) for Docker
        id: meta
        uses: docker/metadata-action@v3
        with:
          images: simachille/ms-zeeven

      - name: Build and push Docker image
        uses: docker/build-push-action@v3
        with:
          context: .
          push: true
          tags: simachille/ms-zeeven:${{ github.sha }}
          labels: ${{ steps.meta.outputs.labels }}

run-staging-container:
name: 'Run staging container' runs-on: ubuntu-latest needs: [dockerize, create-staging-folder]
if: ${{ !(github.base_ref == 'master' || github.ref == 'refs/heads/master') }} environment:
name: staging steps:
- name: Run container uses: appleboy/ssh-action@master with:
host: ${{ secrets.APPLICATIONS_HOST }} port: ${{ secrets.APPLICATIONS_PORT }} username: ${{
secrets.APPLICATIONS_USERNAME }} key: ${{ secrets.APPLICATIONS_SSH_PRIVATE_KEY }} script: | docker compose -f
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml rm -f
docker compose -f
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml stop
docker rmi -f simachille/ms-zeeven:${{ github.sha }} rm -f
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo MAXMIND_ID=${{ secrets.MAXMIND_ID }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo MAXMIND_KEY=${{ secrets.MAXMIND_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo WHATSAPP_TOKEN=${{ secrets.WHATSAPP_TOKEN }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo NOTIFICATIONS_HOST=${{ secrets.NOTIFICATIONS_HOST }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo ZEEVEN_HOST=${{ secrets.ZEEVEN_HOST }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo STRIPE_PUBLIC_KEY=${{ secrets.STRIPE_PUBLIC_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo STRIPE_SECRET_KEY=${{ secrets.STRIPE_SECRET_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo STRIPE_WEBHOOKSECRET_KEY=${{ secrets.STRIPE_WEBHOOKSECRET_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo IPLOCATION_API_KEY=${{ secrets.IPLOCATION_API_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo BACKOFFICE_API_KEY=${{ secrets.BACKOFFICE_API_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo RABBITMQ_IP=${{ secrets.RABBITMQ_IP }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo RABBITMQ_PORT=${{ secrets.RABBITMQ_PORT }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo RABBITMQ_USERNAME=${{ secrets.RABBITMQ_USERNAME }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo RABBITMQ_PASSWORD=${{ secrets.RABBITMQ_PASSWORD }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml pull
            docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml up -d
            cat /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            rm -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

run-prod-container:
name: 'Run prod container' runs-on: ubuntu-latest needs: [dockerize, create-prod-folder]
if: ${{ (github.base_ref == 'master' || github.ref == 'refs/heads/master') }} environment:
name: production steps:
- name: Run container uses: appleboy/ssh-action@master with:
host: ${{ secrets.APPLICATIONS_HOST }} port: ${{ secrets.APPLICATIONS_PORT }} username: ${{
secrets.APPLICATIONS_USERNAME }} key: ${{ secrets.APPLICATIONS_SSH_PRIVATE_KEY }} script: | docker compose -f
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml rm -f
docker compose -f
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml stop
docker rmi -f simachille/ms-zeeven:${{ github.sha }} rm -f
/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo MAXMIND_ID=${{ secrets.MAXMIND_ID }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo MAXMIND_KEY=${{ secrets.MAXMIND_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo WHATSAPP_TOKEN=${{ secrets.WHATSAPP_TOKEN }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo NOTIFICATIONS_HOST=${{ secrets.NOTIFICATIONS_HOST }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo ZEEVEN_HOST=${{ secrets.ZEEVEN_HOST }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo STRIPE_PUBLIC_KEY=${{ secrets.STRIPE_PUBLIC_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo STRIPE_SECRET_KEY=${{ secrets.STRIPE_SECRET_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo STRIPE_WEBHOOKSECRET_KEY=${{ secrets.STRIPE_WEBHOOKSECRET_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo IPLOCATION_API_KEY=${{ secrets.IPLOCATION_API_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo BACKOFFICE_API_KEY=${{ secrets.BACKOFFICE_API_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            echo RABBITMQ_IP=${{ secrets.RABBITMQ_IP }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo RABBITMQ_PORT=${{ secrets.RABBITMQ_PORT }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo RABBITMQ_USERNAME=${{ secrets.RABBITMQ_USERNAME }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            echo RABBITMQ_PASSWORD=${{ secrets.RABBITMQ_PASSWORD }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

            docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml pull
            docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml up -d
            cat /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
            rm -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
