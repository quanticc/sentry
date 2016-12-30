(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('NavbarController', NavbarController);

    NavbarController.$inject = ['$state', 'Auth', 'Principal', 'ProfileService', 'LoginService', 'Discord'];

    function NavbarController ($state, Auth, Principal, ProfileService, LoginService, Discord) {
        var vm = this;

        vm.isNavbarCollapsed = true;
        vm.isAuthenticated = Principal.isAuthenticated;

        ProfileService.getProfileInfo().then(function(response) {
            vm.inProduction = response.inProduction;
            vm.swaggerEnabled = response.swaggerEnabled;
        });

        vm.login = login;
        vm.logout = logout;
        vm.toggleNavbar = toggleNavbar;
        vm.collapseNavbar = collapseNavbar;
        vm.$state = $state;

        function login() {
            collapseNavbar();
            LoginService.open();
        }

        function logout() {
            collapseNavbar();
            Auth.logout();
            $state.go('home');
        }

        function toggleNavbar() {
            vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
        }

        function collapseNavbar() {
            vm.isNavbarCollapsed = true;
        }

        if (vm.isAuthenticated) {
            Discord.getDiscordInfo().then(function(response) {
                if (response.username) {
                    vm.discord = {};
                    vm.discord.username = response.username;
                    vm.discord.avatar = response.avatar;
                } else {
                    vm.discord = {};
                }
            });
        }
    }
})();
