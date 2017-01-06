(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PrivilegeDetailController', PrivilegeDetailController);

    PrivilegeDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Privilege'];

    function PrivilegeDetailController($scope, $rootScope, $stateParams, previousState, entity, Privilege) {
        var vm = this;

        vm.privilege = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('sentryApp:privilegeUpdate', function(event, result) {
            vm.privilege = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
