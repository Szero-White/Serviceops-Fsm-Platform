import { Checkbox, Select } from 'antd'

export interface CheckboxFilterOption {
  value: string
  label: string
}

interface CheckboxFilterSelectProps {
  value: string[]
  options: CheckboxFilterOption[]
  onChange: (value: string[]) => void
  placeholder: string
  ariaLabel?: string
  minWidth?: number
  searchable?: boolean
}

export function CheckboxFilterSelect({
  value,
  options,
  onChange,
  placeholder,
  ariaLabel,
  minWidth = 190,
  searchable = false,
}: CheckboxFilterSelectProps) {
  return (
    <Select<string[]>
      aria-label={ariaLabel}
      mode="multiple"
      allowClear
      showSearch={searchable}
      optionFilterProp="label"
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      options={options}
      maxTagCount={1}
      maxTagPlaceholder={(omitted) => `+${omitted.length}`}
      menuItemSelectedIcon={null}
      style={{ minWidth }}
      optionRender={(option) => (
        <Checkbox checked={value.includes(String(option.value))} style={{ pointerEvents: 'none' }}>
          {option.label}
        </Checkbox>
      )}
    />
  )
}
