(function() {
    'use strict';
    angular
        .module('sentryApp')
        .factory('Subscriber', Subscriber);

    Subscriber.$inject = ['$resource'];

    function Subscriber ($resource) {
        var resourceUrl =  'api/subscribers/:id';

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
