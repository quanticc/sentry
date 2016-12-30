(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PermissionDialogController', PermissionDialogController);

    PermissionDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Permission'];

    function PermissionDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Permission) {
        var vm = this;

        vm.permission = entity;
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
            if (vm.permission.id !== null) {
                Permission.update(vm.permission, onSaveSuccess, onSaveError);
            } else {
                Permission.save(vm.permission, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:permissionUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
