(function () {
    'use strict';

    angular
        .module('sentryApp')
        .controller('UserCountController', UserCountController);

    UserCountController.$inject = ['$scope', '$state', '$interval', '$timeout', '$cookies', 'UserCount', 'Setting', 'ParseLinks', 'AlertService', 'paginationConstants', 'pagingParams'];

    function UserCountController($scope, $state, $interval, $timeout, $cookies, UserCount, Setting, ParseLinks, AlertService, paginationConstants, pagingParams) {
        var vm = this;

        vm.refresher = $interval(updateTime, 1000);
        vm.nextRefresh = 60;

        vm.bot = '...';
        vm.guild = '...';

        loadTimestamps();

        vm.updateRange = updateRange;
        vm.backward = backward;
        vm.forward = forward;
        vm.toggleLive = toggleLive;
        vm.isPresent = isPresent;
        vm.now = now;
        vm.isNow = isNow;

        function loadTimestamps() {
            var storedFrom = $cookies.get('userCountFrom');
            var storedTo = $cookies.get('userCountTo');
            var storedMode = $cookies.get('userCountMode');

            var from = storedFrom == null ? null : moment(Number(storedFrom));
            var to = storedTo == null ? null : moment(Number(storedTo));

            if (from == null || !from.isValid()) {
                from = moment().subtract(1, "hour");
            }

            if (to == null || !to.isValid()) {
                to = moment();
            }

            vm.fromTime = from;
            vm.toTime = to;
            vm.mode = isValidMode(storedMode) ? storedMode : '1h';
            vm.live = vm.mode === '1h' || vm.mode === '4h';
        }

        function isValidMode(mode) {
            return mode === '1h' || mode === '4h' || mode === '1d' || mode === '2d' || mode === '1w' || mode === '1m';
        }

        function now() {
            vm.mode = '1h';
            vm.live = true;
            vm.fromTime = moment().subtract(1, "hour");
            vm.toTime = moment();
            loadAll();
        }

        function isNow() {
            return vm.mode === '1h' && vm.live && isPresent();
        }

        function updateRange(unit) {
            vm.mode = unit;
            if (unit === '1h') {
                vm.fromTime = moment().subtract(1, "hour");
                vm.toTime = moment();
                vm.live = true;
            } else if (unit === '4h') {
                vm.fromTime = moment().subtract(4, "hour");
                vm.toTime = moment();
                vm.live = true;
            } else if (unit === '1d') {
                vm.fromTime = moment().subtract(1, "day");
                vm.toTime = moment();
                vm.live = false;
            } else if (unit === '2d') {
                vm.fromTime = moment().subtract(2, "day");
                vm.toTime = moment();
                vm.live = false;
            } else if (unit === '1w') {
                vm.fromTime = moment().subtract(1, "week");
                vm.toTime = moment();
                vm.live = false;
            } else if (unit === '1m') {
                vm.fromTime = moment().subtract(1, "month");
                vm.toTime = moment();
                vm.live = false;
            }
            loadAll();
        }

        function backward() {
            console.log('<< Going backwards <<');
            var unit = vm.mode;
            if (unit === '1h') {
                vm.fromTime = vm.fromTime.subtract(1, "hour");
                vm.toTime = vm.toTime.subtract(1, "hour");
            } else if (unit === '4h') {
                vm.fromTime = vm.fromTime.subtract(4, "hour");
                vm.toTime = vm.toTime.subtract(4, "hour");
            } else if (unit === '1d') {
                vm.fromTime = vm.fromTime.subtract(1, "day");
                vm.toTime = vm.toTime.subtract(1, "day");
            } else if (unit === '2d') {
                vm.fromTime = vm.fromTime.subtract(2, "day");
                vm.toTime = vm.toTime.subtract(2, "day");
            } else if (unit === '1w') {
                vm.fromTime = vm.fromTime.subtract(1, "week");
                vm.toTime = vm.toTime.subtract(1, "week");
            } else if (unit === '1m') {
                vm.fromTime = vm.fromTime.subtract(1, "month");
                vm.toTime = vm.toTime.subtract(1, "month");
            }
            vm.live = false;
            loadAll();
        }

        function forward() {
            console.log('>> Going forward >>');
            var unit = vm.mode;
            if (unit === '1h') {
                vm.fromTime = vm.fromTime.add(1, "hour");
                vm.toTime = vm.toTime.add(1, "hour");
            } else if (unit === '4h') {
                vm.fromTime = vm.fromTime.add(4, "hour");
                vm.toTime = vm.toTime.add(4, "hour");
            } else if (unit === '1d') {
                vm.fromTime = vm.fromTime.add(1, "day");
                vm.toTime = vm.toTime.add(1, "day");
            } else if (unit === '2d') {
                vm.fromTime = vm.fromTime.add(2, "day");
                vm.toTime = vm.toTime.add(2, "day");
            } else if (unit === '1w') {
                vm.fromTime = vm.fromTime.add(1, "week");
                vm.toTime = vm.toTime.add(1, "week");
            } else if (unit === '1m') {
                vm.fromTime = vm.fromTime.add(1, "month");
                vm.toTime = vm.toTime.add(1, "month");
            }
            loadAll();
        }

        function toggleLive() {
            vm.live = !vm.live;
            if (vm.live) {
                if (!isPresent() || (vm.mode !== '1h' && vm.mode !== '4h')) {
                    now();
                }
            }
        }

        function isPresent() {
            var unit = vm.mode;
            var to = moment(vm.toTime);
            if (unit === '1h') {
                return to.add(1, "hour").isAfter(moment());
            } else if (unit === '4h') {
                return to.add(4, "hour").isAfter(moment());
            } else if (unit === '1d') {
                return to.add(1, "day").isAfter(moment());
            } else if (unit === '2d') {
                return to.add(2, "day").isAfter(moment());
            } else if (unit === '1w') {
                return to.add(1, "week").isAfter(moment());
            } else if (unit === '1m') {
                return to.add(1, "month").isAfter(moment());
            }
            return false;
        }

        function logState() {
            console.log('Live: ' + vm.live + ', Mode: ' + vm.mode + ', From: ' + vm.fromTime + ', To: ' + vm.toTime);
        }

        $scope.$on('$destroy', function () {
            $interval.cancel(vm.refresher);
            clearTooltip();
        });

        function updateTime() {
            if (vm.nextRefresh === 0) {
                vm.nextRefresh = 60;
                loadLast();
            }
            vm.nextRefresh--;
        }

        loadAll();

        function loadAll() {
            logState();
            Setting.find({guild: 'userCount', key: 'defaultBot'}, onBotFound, onNotFound);

            function onBotFound(botData) {
                Setting.find({guild: 'userCount', key: 'defaultGuild'}, function (guildData) {
                    vm.bot = botData.value;
                    vm.guild = guildData.value;

                    var fromTs = vm.fromTime.format('x');
                    var toTs = vm.toTime.format('x');
                    $cookies.put('userCountFrom', fromTs);
                    $cookies.put('userCountTo', toTs);
                    $cookies.put('userCountMode', vm.mode);

                    UserCount.points({
                        bot: botData.value,
                        guild: guildData.value,
                        from: fromTs,
                        to: toTs
                    }, onSuccess, onError);
                }, onNotFound);
            }

            function onNotFound(error) {
                AlertService.error('Settings userCount:defaultBot and/or userCount:defaultGuild not found, create them!');
            }

            function onSuccess(data) {
                $scope.data = data;
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
                        return series;
                    }
                }
            }
            return index;
        }

        function pushPoint(chartData, point, chartName) {
            // lookup the series index this new point belongs to
            var index = findSeries(chartData, point);

            // iterate through existing series
            for (var series in chartData) {
                if (chartData.hasOwnProperty(series)) {
                    if (isNaN(parseFloat(series)) || !isFinite(series)) {
                        continue; // discard useless fields
                    }

                    var xy = [];
                    xy.push(point.values[0][0]);

                    if (index >= 0) {
                        // the point belongs to an existing series
                        if (series === index) {
                            // the point belongs to this series - push it!
                            xy.push(point.values[0][1]);
                            chartData[series].values.push(angular.copy(xy));
                            console.log('[' + chartName + '] Pushed ' + xy[1] + ' @ ' + xy[0] + ' to existing series #' + series + ': ' + chartData[series].key);
                        }
                    }
                }
            }

            if (index < 0) {
                // the point is from a new series, not currently present on chart
                pushZeroes(chartData, point, chartName);
            }
        }

        function pushZeroes(chartData, point, chartName) {
            var xy = [];
            xy.push(point.values[0][0]);

            console.log('[' + chartName + '] Creating missing series: ' + point.key);
            // zero fill existing series at this x-value
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
            console.log('[' + chartName + '] Pushed ' + xy[1] + ' @ ' + xy[0] + ' to new series #' + series + ': ' + chartData[series].key);
        }

        function loadLast() {
            if (!vm.live) {
                console.log("Paused: not updating");
                return;
            }

            Setting.find({guild: 'userCount', key: 'defaultBot'}, onBotFound, onNotFound);

            function onBotFound(botData) {
                Setting.find({guild: 'userCount', key: 'defaultGuild'}, function (guildData) {
                    vm.bot = botData.value;
                    vm.guild = guildData.value;
                    UserCount.last({
                        bot: botData.value,
                        guild: guildData.value
                    }, onSuccess, onError);
                }, onNotFound);
            }

            function onNotFound(error) {
                AlertService.error('Settings userCount:defaultBot and/or userCount:defaultGuild not found, create them!');
            }

            function onSuccess(data) {
                // for each series this new data includes
                for (var newSeries in data) {
                    // filter out useless fields
                    if (data.hasOwnProperty(newSeries)) {
                        var point = data[newSeries]; // point = { key: ..., values: [...] }

                        if (point == null || point.values == null) {
                            continue; // Discarding invalid or empty series
                        }

                        console.log("New data from '" + point.key + "' series: (" + point.values[0][0] + ", " + point.values[0][1] + ")");
                        pushPoint($scope.data, point, 'UserCount');
                    }
                }

                console.log('Refreshing chart');
                clearTooltip();
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        $scope.options = {
            chart: {
                type: 'lineChart',
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
                },
                forceY: [0]
            }
        };

        clearTooltip();

        function clearTooltip() {
            // $timeout(function() {
            //     $scope.dropdownOpen = false;
            //     $timeout(function() {
            //         d3.selectAll('.nvtooltip').remove();
            //     });
            // }, 500);
        }
    }
})();
