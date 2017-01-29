(function () {
    'use strict';
    angular
        .module('sentryApp')
        .factory('PlayerCount', PlayerCount);

    PlayerCount.$inject = ['$resource', 'DateUtils'];

    function PlayerCount($resource, DateUtils) {
        var resourceUrl = 'api/player-counts/:id';

        return $resource(resourceUrl, {}, {
            'query': {method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                        data.timestamp = DateUtils.convertDateTimeFromServer(data.timestamp);
                    }
                    return data;
                }
            },
            'update': {method: 'PUT'},
            'all': {
                method: 'GET',
                url: 'api/player-counts/all'
            },
            'between': {
                method: 'GET',
                url: 'api/player-counts/between',
                params: {
                    resolution: 30
                }
            },
            'last': {method: 'GET', isArray: true, url: 'api/player-counts/last'}
        });
    }
})();
