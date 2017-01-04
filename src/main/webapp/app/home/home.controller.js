(function () {
    'use strict';

    angular
        .module('sentryApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', 'Discord'];

    function HomeController($scope, Principal, LoginService, Discord) {
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        $scope.$on('authenticationSuccess', function () {
            getAccount();
        });

        getAccount();

        function getAccount() {
            Principal.identity().then(function (account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
                if (vm.isAuthenticated()) {
                    getDiscordInfo();
                }
            });
        }

        function getDiscordInfo() {
            Discord.me(function (data) {
                vm.discord = {};
                if (data.username) {
                    vm.discord.username = data.username;
                    vm.discord.avatar = data.avatarUrl;
                }
            });
        }
    }
})();
