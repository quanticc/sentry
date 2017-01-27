(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('StreamerDetailController', StreamerDetailController);

    StreamerDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Streamer'];

    function StreamerDetailController($scope, $rootScope, $stateParams, previousState, entity, Streamer) {
        var vm = this;

        vm.streamer = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:streamerUpdate', function(event, result) {
            vm.streamer = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
