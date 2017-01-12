(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('FlowDetailController', FlowDetailController);

    FlowDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Flow'];

    function FlowDetailController($scope, $rootScope, $stateParams, previousState, entity, Flow) {
        var vm = this;

        vm.flow = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:flowUpdate', function(event, result) {
            vm.flow = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
