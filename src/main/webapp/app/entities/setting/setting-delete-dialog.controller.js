(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('SettingDeleteController',SettingDeleteController);

    SettingDeleteController.$inject = ['$uibModalInstance', 'entity', 'Setting'];

    function SettingDeleteController($uibModalInstance, entity, Setting) {
        var vm = this;

        vm.setting = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Setting.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
