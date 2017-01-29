(function () {
    'use strict';
    angular
        .module('sentryApp')
        .factory('UserCount', UserCount);

    UserCount.$inject = ['$resource', 'DateUtils'];

    function UserCount($resource, DateUtils) {
        var resourceUrl = 'api/user-counts/:id';

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
            'all': {method: 'GET', url: 'api/user-counts/all'},
            'between': {
                method: 'GET',
                url: 'api/user-counts/between',
                params: {
                    after: 0,
                    before: new Date()
                }
            },
            'last': {method: 'GET', isArray: true, url: 'api/user-counts/last'}
        });
    }
})();
