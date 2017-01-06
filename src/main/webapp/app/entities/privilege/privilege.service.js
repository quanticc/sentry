(function() {
    'use strict';
    angular
        .module('sentryApp')
        .factory('Privilege', Privilege);

    Privilege.$inject = ['$resource'];

    function Privilege ($resource) {
        var resourceUrl =  'api/privileges/:id';

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
            'update': { method:'PUT' }
        });
    }
})();
