(function() {
    'use strict';

    angular
        .module('sentryApp')
        .factory('Discord', Discord);

    Discord.$inject = ['$http'];

    function Discord($http) {

        var discordPromise;

        var service = {
            getDiscordInfo : getDiscordInfo
        };

        return service;

        function getDiscordInfo() {
            if (angular.isUndefined(discordPromise)) {
                discordPromise = $http.get('api/discord-info').then(function(result) {
                    if (result.data.user) {
                        var response = {};
                        response.username = result.data.user.username;
                        response.avatar = result.data.avatarUrl;
                        return response;
                    }
                });
            }
            return discordPromise;
        }
    }
})();
