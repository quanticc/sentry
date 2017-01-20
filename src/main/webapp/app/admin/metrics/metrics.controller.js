(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('JhiMetricsMonitoringController', JhiMetricsMonitoringController);

    JhiMetricsMonitoringController.$inject = ['$scope','JhiMetricsService', '$uibModal'];

    function JhiMetricsMonitoringController ($scope, JhiMetricsService, $uibModal) {
        var vm = this;

        vm.metrics = {};
        vm.refresh = refresh;
        vm.refreshThreadDumpData = refreshThreadDumpData;
        vm.servicesStats = {};
        vm.gameServerStats = {};
        vm.playerStats = {};
        vm.updatingMetrics = true;
        vm.stateAsText = stateAsText;

        vm.refresh();

        $scope.$watch('vm.metrics', function (newValue) {
            vm.servicesStats = {};
            angular.forEach(newValue.timers, function (value, key) {
                if (key.indexOf('web.rest') !== -1 || key.indexOf('service') !== -1) {
                    vm.servicesStats[key] = value;
                }
            });

        });

        $scope.$watch('vm.metrics', function (newValue) {
            vm.gameServerStats = {};
            angular.forEach(newValue.timers, function (value, key) {
                if (key.indexOf('UGC.GameServer') !== -1) {
                    vm.gameServerStats[key] = value;
                }
            });
            angular.forEach(newValue.gauges, function (value, key) {
                if (key.indexOf('UGC.GameServer') !== -1 && key.indexOf('status') !== -1) {
                    vm.gameServerStats[key.replace('status', 'delay')].usage = value;
                }
            });

        });

        $scope.$watch('vm.metrics', function (newValue) {
            vm.playerStats = {};
            angular.forEach(newValue.histograms, function (value, key) {
                if (key.indexOf('UGC.GameServer') !== -1) {
                    vm.playerStats[key] = value;
                }
            });
            angular.forEach(newValue.gauges, function (value, key) {
                if (key.indexOf('UGC.GameServer') !== -1 && key.indexOf('playerCount') !== -1) {
                    vm.playerStats[key.replace('playerCount', 'players')].usage = value;
                }
            });

        });

        function refresh () {
            vm.updatingMetrics = true;
            JhiMetricsService.getMetrics().then(function (promise) {
                vm.metrics = promise;
                vm.updatingMetrics = false;
            }, function (promise) {
                vm.metrics = promise.data;
                vm.updatingMetrics = false;
            });
        }

        function refreshThreadDumpData () {
            JhiMetricsService.threadDump().then(function(data) {
                $uibModal.open({
                    templateUrl: 'app/admin/metrics/metrics.modal.html',
                    controller: 'JhiMetricsMonitoringModalController',
                    controllerAs: 'vm',
                    size: 'lg',
                    resolve: {
                        threadDump: function() {
                            return data;
                        }

                    }
                });
            });
        }

        function stateAsText (value) {
            if (value === 0) {
                return 'Healthy';
            } else if (value === 1) {
                return 'Alert';
            } else if (value === 2) {
                return 'Recovering';
            } else {
                return '?';
            }
        }

    }
})();
