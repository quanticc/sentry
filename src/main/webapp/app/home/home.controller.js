(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'Discord'];

    function HomeController ($scope, Principal, LoginService, $state, Discord) {
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        vm.register = register;
        $scope.$on('authenticationSuccess', function() {
            getAccount();
        });

        getAccount();

        function getAccount() {
            Principal.identity().then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
            });
            Discord.getDiscordInfo().then(function(response) {
                vm.discord = {};
                if (response.username) {
                    vm.discord.username = response.username;
                    vm.discord.avatar = response.avatar;
                } else {
                    vm.discord.username = vm.account.login;
                }
            });
        }
        function register () {
            $state.go('register');
        }
    }
})();
