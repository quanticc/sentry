(function() {
    'use strict';
    angular
        .module('sentryApp')
        .factory('Bot', Bot);

    Bot.$inject = ['$resource'];

    function Bot ($resource) {
        var resourceUrl =  'api/bots/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                    }
                    return data;
                }
            },
            'update': { method: 'PUT' },
            'action': { method: 'POST', url: 'api/bots/:id/:action' },
            'status': { method: 'GET', url: 'api/bots/:id/status',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                    }
                    return data;
                }
            }
        });
    }
})();
