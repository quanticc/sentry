(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('TimeFrameDialogController', TimeFrameDialogController);

    TimeFrameDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'TimeFrame'];

    function TimeFrameDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, TimeFrame) {
        var vm = this;

        vm.timeFrame = entity;
        vm.clear = clear;
        vm.datePickerOpenStatus = {};
        vm.openCalendar = openCalendar;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.timeFrame.id !== null) {
                TimeFrame.update(vm.timeFrame, onSaveSuccess, onSaveError);
            } else {
                TimeFrame.save(vm.timeFrame, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:timeFrameUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }

        vm.datePickerOpenStatus.start = false;
        vm.datePickerOpenStatus.end = false;

        function openCalendar (date) {
            vm.datePickerOpenStatus[date] = true;
        }
    }
})();
