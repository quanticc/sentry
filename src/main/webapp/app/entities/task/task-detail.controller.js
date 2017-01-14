(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('TaskDetailController', TaskDetailController);

    TaskDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Task'];

    function TaskDetailController($scope, $rootScope, $stateParams, previousState, entity, Task) {
        var vm = this;

        vm.task = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:taskUpdate', function(event, result) {
            vm.task = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
