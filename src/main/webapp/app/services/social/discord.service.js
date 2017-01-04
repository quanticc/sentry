(function() {
    'use strict';

    angular
        .module('sentryApp')
        .factory('Discord', Discord);

    Discord.$inject = ['$resource'];

    function Discord($resource) {
        var resourceUrl = 'api/discord';
        var transformer = function (data) {
            if (data) {
                data = angular.fromJson(data);
            }
            return data;
        };

        return $resource(resourceUrl, {}, {
            'me': { method: 'GET', transformResponse: transformer },
            'user': { method: 'GET', url: 'api/discord/user', transformResponse: transformer },
            'guilds': { method: 'GET', url: 'api/discord/guilds', transformResponse: transformer },
            'connections': { method: 'GET', url: 'api/discord/connections', transformResponse: transformer },
            'full': { method: 'GET', url: 'api/discord/full', transformResponse: transformer }
        });
    }
})();
