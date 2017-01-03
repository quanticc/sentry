(function () {
    'use strict';

    angular
        .module('sentryApp')
        .controller('BotController', BotController);

    BotController.$inject = ['$scope', '$state', 'Bot', 'ParseLinks', 'AlertService', 'paginationConstants', 'pagingParams'];

    function BotController($scope, $state, Bot, ParseLinks, AlertService, paginationConstants, pagingParams) {
        var vm = this;

        vm.loadPage = loadPage;
        vm.predicate = pagingParams.predicate;
        vm.reverse = pagingParams.ascending;
        vm.transition = transition;
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.statusText = statusText;
        vm.statusClass = statusClass;

        loadAll();

        function loadAll() {
            Bot.query({
                page: pagingParams.page - 1,
                size: vm.itemsPerPage,
                sort: sort()
            }, onSuccess, onError);
            function sort() {
                var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
                if (vm.predicate !== 'id') {
                    result.push('id');
                }
                return result;
            }

            function onSuccess(data, headers) {
                vm.links = ParseLinks.parse(headers('link'));
                vm.totalItems = headers('X-Total-Count');
                vm.queryCount = vm.totalItems;
                vm.bots = data;
                vm.page = pagingParams.page;
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        function loadPage(page) {
            vm.page = page;
            vm.transition();
        }

        function transition() {
            $state.transitionTo($state.$current, {
                page: vm.page,
                sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
                search: vm.currentSearch
            });
        }

        function statusText(bot) {
            if (bot.created) {
                if (bot.loggedIn) {
                    if (bot.ready) {
                        return 'ONLINE';
                    } else {
                        return 'LOADING';
                    }
                }
            }
            return 'OFFLINE';
        }

        function statusClass(bot) {
            if (bot.created) {
                if (bot.loggedIn) {
                    if (bot.ready) {
                        return 'label-success';
                    } else {
                        return 'label-warning';
                    }
                }
            }
            return 'label-default';
        }
    }
})();
