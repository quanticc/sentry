# Sentry

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cc33fe55d2454684bc537690e4cda0a1)](https://www.codacy.com/app/quanticc/sentry?utm_source=github.com&utm_medium=referral&utm_content=quanticc/sentry&utm_campaign=badger)

This application is an assistant for [UGC League][] operations and the successor for the [ugc-bot][] project.
Bootstrapped using [JHipster 3.12.2][] (Spring Boot + AngularJS).

## Features

- Exposes a web administration panel with OAuth2 support
- Manages [Discord][] bots to communicate with general users
- Delivers server files (maps, config files) and game version updates in batch
- Retrieves SourceTV and log files and uploads them to Dropbox
- Keeps track of GS server statistics and expire status
- Matches UGC result data with stats providers
- Alerts on missing updates and unresponsive servers

## Development

Requirements to build: [Node.js][], JDK 8.

    npm install
    npm install -g gulp-cli
    
## Production builds

    ./gradlew -Pprod clean release -Prelease.stage=final
    java -jar build/libs/*.war
    
## Using Docker

To launch a MongoDB instance:

    docker-compose -f src/main/docker/mongodb.yml up -d

[UGC League]: http://www.ugcleague.com/
[ugc-bot]: https://github.com/quanticc/ugc-bot-redux
[Discord]: https://discordapp.com/

[JHipster 3.12.2]: https://jhipster.github.io/documentation-archive/v3.12.2
[Node.js]: https://nodejs.org/
