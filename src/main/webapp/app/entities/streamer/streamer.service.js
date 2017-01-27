(function() {
    'use strict';
    angular
        .module('sentryApp')
        .factory('Streamer', Streamer);

    Streamer.$inject = ['$resource', 'DateUtils'];

    function Streamer ($resource, DateUtils) {
        var resourceUrl =  'api/streamers/:id';

        return $resource(resourceUrl, {}, {
            'query': { method: 'GET', isArray: true},
            'get': {
                method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        data = angular.fromJson(data);
                        data.lastAnnouncement = DateUtils.convertDateTimeFromServer(data.lastAnnouncement);
                    }
                    return data;
                }
            },
            'update': { method:'PUT' }
        });
    }
})();
