(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('StreamerController', StreamerController);

    StreamerController.$inject = ['$scope', '$state', 'Streamer', 'ParseLinks', 'AlertService', 'paginationConstants', 'pagingParams'];

    function StreamerController ($scope, $state, Streamer, ParseLinks, AlertService, paginationConstants, pagingParams) {
        var vm = this;

        vm.loadPage = loadPage;
        vm.predicate = pagingParams.predicate;
        vm.reverse = pagingParams.ascending;
        vm.transition = transition;
        vm.itemsPerPage = paginationConstants.itemsPerPage;

        vm.prettyBoolText = prettyBoolText;
        vm.prettyBoolClass = prettyBoolClass;

        loadAll();

        function loadAll () {
            Streamer.query({
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
                vm.streamers = data;
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

        function prettyBoolText(value) {
            if (value) {
                return 'YES';
            } else {
                return 'NO';
            }
        }

        function prettyBoolClass(value) {
            if (value) {
                return 'label-success';
            } else {
                return 'label-danger';
            }
        }
    }
})();
