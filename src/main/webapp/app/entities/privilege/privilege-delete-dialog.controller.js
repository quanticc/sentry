(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PrivilegeDeleteController',PrivilegeDeleteController);

    PrivilegeDeleteController.$inject = ['$uibModalInstance', 'entity', 'Privilege'];

    function PrivilegeDeleteController($uibModalInstance, entity, Privilege) {
        var vm = this;

        vm.privilege = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Privilege.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
