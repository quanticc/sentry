(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PrivilegeDialogController', PrivilegeDialogController);

    PrivilegeDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Privilege'];

    function PrivilegeDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Privilege) {
        var vm = this;

        vm.privilege = entity;
        vm.clear = clear;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.privilege.id !== null) {
                Privilege.update(vm.privilege, onSaveSuccess, onSaveError);
            } else {
                Privilege.save(vm.privilege, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:privilegeUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
