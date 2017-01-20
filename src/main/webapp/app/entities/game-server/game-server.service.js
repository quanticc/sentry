(function() {
    'use strict';
    angular
        .module('sentryApp')
        .factory('GameServer', GameServer);

    GameServer.$inject = ['$resource', 'DateUtils'];

    function GameServer ($resource, DateUtils) {
        var resourceUrl =  'api/game-servers/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                        data.expirationDate = DateUtils.convertDateTimeFromServer(data.expirationDate);
                        data.expirationCheckDate = DateUtils.convertDateTimeFromServer(data.expirationCheckDate);
                        data.statusCheckDate = DateUtils.convertDateTimeFromServer(data.statusCheckDate);
                        data.lastValidPing = DateUtils.convertDateTimeFromServer(data.lastValidPing);
                        data.lastRconDate = DateUtils.convertDateTimeFromServer(data.lastRconDate);
                        data.lastGameUpdate = DateUtils.convertDateTimeFromServer(data.lastGameUpdate);
                        data.lastUpdateStart = DateUtils.convertDateTimeFromServer(data.lastUpdateStart);
                        data.lastRconAnnounce = DateUtils.convertDateTimeFromServer(data.lastRconAnnounce);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' },
            'refresh': {
                method: 'POST', url: 'api/game-servers/refresh'
            }
        });
    }
})();
