(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('GameServerController', GameServerController);

    GameServerController.$inject = ['$scope', '$state', 'GameServer', 'ParseLinks', 'AlertService', 'paginationConstants'];

    function GameServerController ($scope, $state, GameServer, ParseLinks, AlertService, paginationConstants) {
        var vm = this;

        vm.gameServers = [];
        vm.loadPage = loadPage;
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.page = 0;
        vm.links = {
            last: 0
        };
        vm.predicate = 'name';
        vm.reset = reset;
        vm.reverse = true;

        vm.shortName = shortName;
        vm.pingToText = pingToText;
        vm.grayIfZero = grayIfZero;
        vm.formatMap = formatMap;
        vm.pingToClass = pingToClass;
        vm.grayIfFalse = grayIfFalse;
        vm.prettyBoolText = prettyBoolText;
        vm.prettyBoolClass = prettyBoolClass;

        loadAll();

        function loadAll () {
            GameServer.query({
                page: vm.page,
                size: vm.itemsPerPage,
                sort: sort()
            }, onSuccess, onError);
            function sort() {
                var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
                if (vm.predicate !== 'name') {
                    result.push('name');
                }
                return result;
            }

            function onSuccess(data, headers) {
                vm.links = ParseLinks.parse(headers('link'));
                vm.totalItems = headers('X-Total-Count');
                for (var i = 0; i < data.length; i++) {
                    vm.gameServers.push(data[i]);
                }
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        function reset () {
            vm.page = 0;
            vm.gameServers = [];
            loadAll();
        }

        function loadPage(page) {
            vm.page = page;
            loadAll();
        }

        function shortName(value) {
            return value.replace(/(^[A-Za-z]{3})[^0-9]*([0-9]+).*/, "$1$2").toUpperCase();
        }

        function pingToText(ping) {
            if (ping > 0 && ping < 1000) {
                return 'UP';
            } else {
                return 'DOWN';
            }
        }

        function grayIfZero(number) {
            if (number > 0) {
                return '';
            } else {
                return 'gray';
            }
        }

        function formatMap(name) {
            if (name != null && name != '') {
                return ' @ ' + name;
            } else {
                return '';
            }
        }

        function pingToClass(ping) {
            if (ping > 0) {
                return 'label-success';
            } else {
                return 'label-danger';
            }
        }

        function grayIfFalse(flag) {
            if (flag === true) {
                return '';
            } else {
                return 'gray';
            }
        }

        function prettyBoolText(value) {
            if (value) {
                return 'YES';
            } else {
                return 'NO';
            }
        }

        function prettyBoolClass(value) {
            if (value) {
                return 'label-warning';
            } else {
                return 'label-info';
            }
        }
    }
})();
