(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PermissionDetailController', PermissionDetailController);

    PermissionDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Permission'];

    function PermissionDetailController($scope, $rootScope, $stateParams, previousState, entity, Permission) {
        var vm = this;

        vm.permission = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:permissionUpdate', function(event, result) {
            vm.permission = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
