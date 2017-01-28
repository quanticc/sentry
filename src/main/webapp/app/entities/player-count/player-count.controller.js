(function () {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PlayerCountController', PlayerCountController);

    PlayerCountController.$inject = ['$scope', '$state', 'PlayerCount', 'ParseLinks', 'AlertService'];

    function PlayerCountController($scope, $state, PlayerCount, ParseLinks, AlertService) {

        loadAll();

        function loadAll() {
            PlayerCount.pastDay({}, onSuccess, onError);

            function onSuccess(data) {
                $scope.data = data;
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }

        $scope.options = {
            chart: {
                type: 'stackedAreaChart',
                height: 450,
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 60,
                    left: 40
                },
                x: function(d){return d[0];},
                y: function(d){return d[1];},
                useVoronoi: false,
                clipEdge: true,
                duration: 100,
                useInteractiveGuideline: true,
                xAxis: {
                    showMaxMin: false,
                    tickFormat: function(d) {
                        return d3.time.format('%y-%m-%d %H:%M')(new Date(d * 1000))
                    }
                },
                yAxis: {
                    tickFormat: function(d){
                        return d3.format('d')(d);
                    }
                }
            }
        };

    }
})();
