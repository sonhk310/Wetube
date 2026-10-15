import { useForm, type SubmitHandler } from "react-hook-form";
import { yupResolver } from "@hookform/resolvers/yup";
import { RHFRegisterFormSchema } from "@/schema/RHFRegisterForm.schama";
import type { RHFRegisterFormValues } from "../../../types/managerTypes/registerManager";

interface Props {
  defaultValues: RHFRegisterFormValues;
  onSubmit: (data: RHFRegisterFormValues) => void;
  isLoading: boolean;
}

export const UpdateManagerForm: React.FC<Props> = ({
  defaultValues,
  onSubmit,
  isLoading,
}) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<RHFRegisterFormValues>({
    resolver: yupResolver(RHFRegisterFormSchema),
    defaultValues: defaultValues,
  });

  const submit: SubmitHandler<RHFRegisterFormValues> = (data) => {
    onSubmit(data);
    reset(defaultValues); // reset lại theo giá trị gốc sau khi update
  };

  return (
    <form
      onSubmit={handleSubmit(submit)}
      className="p-6 bg-white rounded-xl shadow-md space-y-4"
    >
      <h2 className="text-xl font-semibold mb-4 text-gray-800">
        Update Manager Information
      </h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Name */}
        <div>
          <input
            {...register("name")}
            placeholder="Full Name"
            className="w-full p-2 border rounded-md"
          />
          {errors.name && (
            <p className="text-red-500 text-sm">{errors.name.message}</p>
          )}
        </div>

        {/* Email */}
        <div>
          <input
            {...register("email")}
            placeholder="Email"
            className="w-full p-2 border rounded-md"
          />
          {errors.email && (
            <p className="text-red-500 text-sm">{errors.email.message}</p>
          )}
        </div>

        {/* Phone */}
        <div>
          <input
            {...register("phone")}
            placeholder="Phone Number"
            className="w-full p-2 border rounded-md"
          />
          {errors.phone && (
            <p className="text-red-500 text-sm">{errors.phone.message}</p>
          )}
        </div>

        {/* Date of Birth */}
        <div>
          <input
            {...register("dateOfBirth")}
            type="date"
            className="w-full p-2 border rounded-md"
          />
          {errors.dateOfBirth && (
            <p className="text-red-500 text-sm">
              {errors.dateOfBirth.message}
            </p>
          )}
        </div>

        {/* Gender */}
        <div>
          <select
            {...register("gender")}
            className="w-full p-2 border rounded-md"
          >
            <option value="male">Male</option>
            <option value="female">Female</option>
            <option value="other">Other</option>
          </select>
          {errors.gender && (
            <p className="text-red-500 text-sm">{errors.gender.message}</p>
          )}
        </div>

        {/* Role */}
        <div>
          <select
            {...register("role")}
            className="w-full p-2 border rounded-md"
          >
            <option value="manager">Manager</option>
            <option value="supervisor">Supervisor</option>
            <option value="admin">Admin</option>
          </select>
          {errors.role && (
            <p className="text-red-500 text-sm">{errors.role.message}</p>
          )}
        </div>

        {/* Status */}
        <div>
          <select
            {...register("status")}
            className="w-full p-2 border rounded-md"
          >
            <option value="pending">Pending</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
          {errors.status && (
            <p className="text-red-500 text-sm">{errors.status.message}</p>
          )}
        </div>

        {/* Address */}
        <div className="md:col-span-2">
          <input
            {...register("address")}
            placeholder="Address"
            className="w-full p-2 border rounded-md"
          />
          {errors.address && (
            <p className="text-red-500 text-sm">{errors.address.message}</p>
          )}
        </div>
      </div>

      {/* Update button */}
      <button
        type="submit"
        disabled={isLoading}
        className={`w-full py-3 rounded-lg font-semibold transition-colors duration-200
          ${
            isLoading
              ? "bg-blue-400 text-white cursor-not-allowed"
              : "bg-blue-600 hover:bg-blue-700 text-white shadow-md"
          }`}
      >
        {isLoading ? (
          <span className="flex items-center justify-center gap-2">
            <svg
              className="animate-spin h-5 w-5 text-white"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              ></circle>
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
              ></path>
            </svg>
            Updating...
          </span>
        ) : (
          "Update"
        )}
      </button>
    </form>
  );
};
