(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('TimeFrameDetailController', TimeFrameDetailController);

    TimeFrameDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'TimeFrame'];

    function TimeFrameDetailController($scope, $rootScope, $stateParams, previousState, entity, TimeFrame) {
        var vm = this;

        vm.timeFrame = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:timeFrameUpdate', function(event, result) {
            vm.timeFrame = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
