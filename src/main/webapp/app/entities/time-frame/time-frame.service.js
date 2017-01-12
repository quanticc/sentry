(function() {
    'use strict';
    angular
        .module('sentryApp')
        .factory('TimeFrame', TimeFrame);

    TimeFrame.$inject = ['$resource', 'DateUtils'];

    function TimeFrame ($resource, DateUtils) {
        var resourceUrl =  'api/time-frames/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                        data.start = DateUtils.convertDateTimeFromServer(data.start);
                        data.end = DateUtils.convertDateTimeFromServer(data.end);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' }
        });
    }
})();
