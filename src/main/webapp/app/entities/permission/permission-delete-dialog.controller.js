(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PermissionDeleteController',PermissionDeleteController);

    PermissionDeleteController.$inject = ['$uibModalInstance', 'entity', 'Permission'];

    function PermissionDeleteController($uibModalInstance, entity, Permission) {
        var vm = this;

        vm.permission = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Permission.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
