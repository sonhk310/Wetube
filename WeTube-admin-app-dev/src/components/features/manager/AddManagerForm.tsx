import type { RHFRegisterFormValues } from "../../../types/managerTypes/registerManager";
import { RHFRegisterFormSchema } from "@/schema/RHFRegisterForm.schama";
import { yupResolver } from '@hookform/resolvers/yup';
import { useForm, type SubmitHandler } from "react-hook-form";

interface AddManagerFormProps {
  onSubmit: (data: RHFRegisterFormValues) => void;
}

const AddManagerForm: React.FC<AddManagerFormProps> = ({onSubmit}) => {
  const {
  register,
  handleSubmit,
  formState: { errors },
  reset,
} = useForm<RHFRegisterFormValues>({
  resolver: yupResolver(RHFRegisterFormSchema),
});
  //  const submit: SubmitHandler<RHFRegisterFormValues> = (data) => {
  //   onSubmit(data);
  //   reset();
  // };

    const handleFormSubmit: SubmitHandler<RHFRegisterFormValues> = (data) => {
      onSubmit(data);
      reset(); 
    };

return (
  <form
    onSubmit={handleSubmit(handleFormSubmit)}
    className="space-y-4 p-6 bg-white rounded-xl shadow-mdtransition-all duration-200 w-full"
  >
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="flex flex-col">
        <input
          {...register("name")}
          type="text"
          placeholder="Full Name"
          className="p-2 w-full border rounded-md"
        />
        {errors.name && (
          <p className="text-red-500 text-sm mt-1"> {errors.name.message}</p>
        )}
      </div>
      <div className="flex flex-col">
        <input
          {...register("email")}
          type="email"
          placeholder="Email"
          className="p-2 border rounded-md"
        />
        {errors.email && <p className="text-red-500">{errors.email.message}</p>}
      </div>

      <div className="flex flex-col">
        <input
          {...register("phone")}
          type="tel"
          placeholder="Phone"
          className="p-2 border rounded-md"
        />
        {errors.phone && <p className="text-red-500">{errors.phone.message}</p>}
      </div>
      <div className="flex flex-col">
        <select
          {...register("gender")}
          className="p-2 border rounded-md"
        >
          <option value="">Select Gender</option>
          <option value="male">Male</option>
          <option value="female">Female</option>
          <option value="other">Other</option>
        </select>
        {errors.gender && (
          <p className="text-red-500">{errors.gender.message}</p>
        )}
      </div>

      <div className="flex flex-col">
        <input
          {...register("password")}
          type="password"
          placeholder="Password"
          className="p-2 border rounded-md"
        />
        {errors.password && (
          <p className="text-red-500">{errors.password.message}</p>
        )}
      </div>

      <div className="flex flex-col">
        <input
          {...register("confirmPassword")}
          type="password"
          placeholder="Confirm Password"
          className="p-2 border rounded-md"
        />
        {errors.confirmPassword && (
          <p className="text-red-500">{errors.confirmPassword.message}</p>
        )}
      </div>
      <div className="flex flex-col">
        <input
          {...register("dateOfBirth")}
          type="date"
          placeholder="Date of Birth"
          className="p-2 border rounded-md"
        />
        {errors.dateOfBirth && (
          <p className="text-red-500">{errors.dateOfBirth.message}</p>
        )}
      </div>
       <div className="flex flex-col">
          <input
            {...register("address")}
            type="text"
            placeholder="Address"
            className="p-2 border rounded-md"
          />
          {errors.address && (
            <p className="text-red-500 text-sm mt-1">{errors.address.message}</p>
          )}
        </div>

        {/* Role */}
        <div className="flex flex-col">
          <select {...register("role")} className="p-2 border rounded-md">
            <option value="">Select Role</option>
            <option value="manager">Manager</option>
            <option value="admin">Admin</option>
          </select>
          {errors.role && (
            <p className="text-red-500 text-sm mt-1">{errors.role.message}</p>
          )}
        </div>
      <select
        {...register("status")}
        className="p-2 border rounded-md"
      >
        <option value="Pending">Pending</option>
      </select>
      {errors.status && <p className="text-red-500">{errors.status.message}</p>}
    </div>

    <button
      type="submit"
      className="w-full py-2 rounded-md text-white font-medium bg-green-500 hover:bg-yellow-500"
    >
      Create Manager
    </button>
  </form>
);
};
export default AddManagerForm;