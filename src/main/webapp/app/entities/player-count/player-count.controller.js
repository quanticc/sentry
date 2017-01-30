(function () {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PlayerCountController', PlayerCountController);

    PlayerCountController.$inject = ['$scope', '$state', '$interval', '$timeout', 'PlayerCount', 'ParseLinks', 'AlertService'];

    function PlayerCountController($scope, $state, $interval, $timeout, PlayerCount, ParseLinks, AlertService) {
        var vm = this;

        vm.refresher = $interval(loadLast, 60000);
        vm.nextRefresh = 60;
        vm.clock = $interval(updateTime, 1000);

        $scope.$on('$destroy', function () {
            $interval.cancel(vm.refresher);
            $interval.cancel(vm.clock);
            clearTooltip();
        });

        function updateTime() {
            if (vm.nextRefresh === 0) {
                vm.nextRefresh = 60;
            }
            vm.nextRefresh--;
        }

        loadAll();

        function loadAll() {
            PlayerCount.all({}, onSuccess, onError);

            function onSuccess(data) {
                $scope.pastDayData = data.day;
                $scope.pastWeekData = data.week;
                $scope.pastMonthData = data.month;
                $scope.pastYearData = data.year;
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        function findSeries(chartData, point) {
            var index = -1;
            for (var series in chartData) {
                if (chartData.hasOwnProperty(series)) {
                    var set = chartData[series];
                    if (point.key === set.key) {
                        index = series;
                    }
                }
            }
            return index;
        }

        function pushPoint(chartData, point, chartName) {
            // lookup the series index this new point belongs to
            var index = findSeries(chartData, point);

            // if point belong to an existing series, add it and pad the rest with y=0
            // if it doesn't belong, create a new series, add it and pad the others with y=0
            for (var series in chartData) {
                if (chartData.hasOwnProperty(series)) {
                    var xy = [];
                    xy.push(point.values[0][0]);

                    if (index >= 0) {
                        if (series === index) {
                            xy.push(point.values[0][1]);
                            chartData[series].values.push(angular.copy(xy));
                        } else {
                            xy.push(0);
                            chartData[series].values.push(angular.copy(xy));
                        }
                        console.log('[' + chartName + '] Pushed ' + xy[1] + ' @ ' + xy[0] + ' to ' + series);
                    } else {
                        console.log('[' + chartName + '] Creating missing series: ' + point.key);
                        // zero fill
                        for (var innerSeries in chartData) {
                            if (chartData.hasOwnProperty(innerSeries)) {
                                var zero = [];
                                zero.push(point.values[0][0]);
                                zero.push(0);
                                chartData[innerSeries].values.push(angular.copy(zero));
                            }
                        }
                        // series must be created
                        xy.push(point.values[0][1]);
                        var item = {};
                        item.key = point.key;
                        item.values = [];
                        item.values.push(xy);
                        chartData.push(angular.copy(item));
                        console.log('[' + chartName + '] Pushed ' + xy[1] + ' @ ' + xy[0] + ' to ' + series);
                    }
                }
            }
        }

        function loadLast() {
            PlayerCount.last({}, onSuccess, onError);

            function onSuccess(data) {
                for (var newSeries in data) {
                    if (data.hasOwnProperty(newSeries)) {
                        var point = data[newSeries];

                        if (point == null || point.values == null) {
                            console.log('Incoming data is empty - aborting');
                            continue;
                        }

                        pushPoint($scope.pastDayData, point, 'day');
                        pushPoint($scope.pastWeekData, point, 'week');
                        pushPoint($scope.pastMonthData, point, 'month');
                        pushPoint($scope.pastYearData, point, 'year');

                        console.log('Refreshing chart');
                        updateDomains();
                        clearTooltip();
                    }
                }
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        // $scope.config = {
        //     deepWatchData: true
        // };

        var options = {
            chart: {
                type: 'stackedAreaChart',
                height: 300,
                margin: {
                    top: 20,
                    right: 20,
                    bottom: 60,
                    left: 40
                },
                x: function (d) {
                    return d[0];
                },
                y: function (d) {
                    return d[1];
                },
                useVoronoi: false,
                clipEdge: true,
                duration: 0,
                useInteractiveGuideline: true,
                xAxis: {
                    showMaxMin: false,
                    tickFormat: function (d) {
                        return d3.time.format('%b %d %H:%M')(new Date(d))
                    }
                },
                yAxis: {
                    tickFormat: function (d) {
                        return d3.format('d')(d);
                    }
                }
            }
        };

        $scope.dayOptions = angular.copy(options);
        $scope.weekOptions = angular.copy(options);
        $scope.monthOptions = angular.copy(options);
        $scope.yearOptions = angular.copy(options);

        updateDomains();

        function updateDomains() {
            $scope.dayOptions.chart.xDomain = [moment().subtract(1, "days").toDate(), new Date()];
            $scope.weekOptions.chart.xDomain = [moment().subtract(1, "weeks").toDate(), new Date()];
            $scope.monthOptions.chart.xDomain = [moment().subtract(1, "months").toDate(), new Date()];
            $scope.yearOptions.chart.xDomain = [moment().subtract(1, "years").toDate(), new Date()];
        }

        clearTooltip();

        function clearTooltip() {
            $timeout(function() {
                $scope.dropdownOpen = false;
                $timeout(function() {
                    d3.selectAll('.nvtooltip').remove();
                });
            }, 500);
        }
    }
})();
